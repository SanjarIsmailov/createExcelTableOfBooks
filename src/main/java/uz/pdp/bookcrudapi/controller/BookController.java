package uz.pdp.bookcrudapi.controller;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.pdp.bookcrudapi.entity.Book;
import uz.pdp.bookcrudapi.repo.BookRepo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/book")
@RequiredArgsConstructor
public class BookController {
    private final BookRepo bookRepo;

    @GetMapping("/")
    public String getAllBooks(Model model) {
        model.addAttribute("books", bookRepo.findAll());
        return "home";
    }

    @GetMapping("/create")
    public String createBookForm(Book book, Model model) {
        model.addAttribute("book", book);
        return "create-book";
    }

    @PostMapping("/create")
    public String createBook(@ModelAttribute Book book, Model model) {
        if (!isValid(book)) {
            model.addAttribute("error", "Invalid book data");
            return "create-book";
        }
        bookRepo.save(book);
        return "redirect:/book/";
    }

    @GetMapping("/edit/{id}")
    public String editBookForm(@PathVariable("id") int id, Model model) {
        Optional<Book> book = bookRepo.findById(id);
        if (book.isPresent()) {
            model.addAttribute("book", book.get());
            return "edit-book";
        } else {
            return "redirect:/book/";
        }
    }

    @PostMapping("/update/{id}")
    public String updateBook(@PathVariable("id") int id, @ModelAttribute Book book, Model model) {
        if (!isValid(book)) {
            model.addAttribute("error", "Invalid book data");
            return "edit-book";
        }
        book.setId(id);
        bookRepo.save(book);
        return "redirect:/book/";
    }

    @GetMapping("/delete/{id}")
    public String deleteBook(@PathVariable("id") int id) {
        bookRepo.deleteById(id);
        return "redirect:/book/";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportBooks() throws IOException {
        List<Book> books = bookRepo.findAll();
        ByteArrayInputStream stream = exportToExcel(books);

        byte[] bytes = stream.readAllBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=books.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping("/import")
    public String importBooks(@RequestParam("file") MultipartFile file) {
        try {
            List<Book> books = importFromExcel(file);
            bookRepo.saveAll(books);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect:/book/";
    }

    private boolean isValid(Book book) {
        return book.getTitle() != null && !book.getTitle().isEmpty() &&
                book.getAuthor() != null && !book.getAuthor().isEmpty() &&
                book.getPrice() > 0;
    }

    private ByteArrayInputStream exportToExcel(List<Book> books) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Books");

            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Title");
            headerRow.createCell(2).setCellValue("Author");
            headerRow.createCell(3).setCellValue("Price");

            int rowIndex = 1;
            for (Book book : books) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(book.getId());
                row.createCell(1).setCellValue(book.getTitle());
                row.createCell(2).setCellValue(book.getAuthor());
                row.createCell(3).setCellValue(book.getPrice());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private List<Book> importFromExcel(MultipartFile file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Book> books = new ArrayList<>();

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                Book book = new Book();
                book.setTitle(row.getCell(1).getStringCellValue());
                book.setAuthor(row.getCell(2).getStringCellValue());
                book.setPrice(row.getCell(3).getNumericCellValue());
                books.add(book);
            }

            return books;
        }
    }
}
