package kz.nurbolat.bookaggregator.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * @param editionIds restrict the run to these editions; omitted or {@code null} for the whole catalog
 *                   (within the run limits)
 */
public record ParserRunRequest(@Schema(description = "Ограничить обход этими изданиями; без поля - весь каталог в пределах лимитов", example = "[1, 2, 3]")
        @Size(max = 200) List<Long> editionIds) {
}
