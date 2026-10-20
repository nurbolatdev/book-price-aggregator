package kz.nurbolat.bookaggregator.source;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookSourceResult {

    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private String coverUrl;
    private String description;

    private String source;
}
