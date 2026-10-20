package kz.nurbolat.bookaggregator.source;

import java.util.List;

public interface BookSource {

    String getName();

    List<BookSourceResult> search(String query);
}
