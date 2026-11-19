package com.programacion.distribuida.books.service.impl;

import com.programacion.distribuida.books.db.Book;
import com.programacion.distribuida.books.dtos.BookDto;
import com.programacion.distribuida.books.repo.BookRepository;
import com.programacion.distribuida.books.service.BookService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    @Inject
    public BookServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    private BookDto toDto(Book book) {
        return BookDto.builder()
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .price(book.getPrice())
                .inventorySold(book.getInventory()!=null? book.getInventory().getSold():0)
                .inventorySupplied(book.getInventory()!=null? book.getInventory().getSupplied():0)
                .build();
    }

    public List<BookDto> findAll() {
        return bookRepository.streamAll()
                .map(this::toDto)
                .toList();
    }

    public Optional<BookDto> findByIdOptional(String isbn) {
        return bookRepository.findByIdOptional(isbn)
                .map(this::toDto);
    }
}