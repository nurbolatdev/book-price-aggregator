package kz.nurbolat.bookaggregator.source;

import java.util.List;

public interface BookSource {

    String getName();

    List<BookSourceResult> search(String query);

    default boolean isDiscoverySource() {
        return true;
    }

    default List<BookSourceResult> enrichBook(String title, String author, String isbn) {
        return List.of();
    }
}
