package uz.pdp.bookcrudapi.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pdp.bookcrudapi.entity.Book;

public interface BookRepo extends JpaRepository<Book, Integer> {
}
