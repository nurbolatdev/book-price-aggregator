package kz.nurbolat.bookaggregator.admin.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * @param editionIds restrict the run to these editions; omitted or {@code null} for the whole catalog
 *                   (within the run limits)
 */
public record ParserRunRequest(@Size(max = 200) List<Long> editionIds) {
}
