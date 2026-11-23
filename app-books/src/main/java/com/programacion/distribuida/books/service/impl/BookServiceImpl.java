package com.programacion.distribuida.books.service.impl;

import com.programacion.distribuida.books.clients.AuthorRestClient;
import com.programacion.distribuida.books.db.Book;
import com.programacion.distribuida.books.dtos.AuthorDto;
import com.programacion.distribuida.books.dtos.BookDto;
import com.programacion.distribuida.books.repo.BookRepository;
import com.programacion.distribuida.books.service.BookService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorRestClient authorRestClient;

    @Inject
    public BookServiceImpl(BookRepository bookRepository, @RestClient AuthorRestClient authorRestClient) {
        this.bookRepository = bookRepository;
        this.authorRestClient = authorRestClient;
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
                .peek(it -> {
                    var authors = authorRestClient.findByBook(it.getIsbn());
                })
                .toList();
    }

    public Optional<BookDto> findByIdOptional(String isbn) {
        List<AuthorDto> authors = authorRestClient.findByBook(isbn);

        return bookRepository.findByIdOptional(isbn)
                .map(this::toDto)
                .map(it -> {
                    it.setAuthors(authors);
                    return it;
                });
    }
}