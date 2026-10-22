# Meloman fixtures

Pages saved on 2026-10-22 from https://www.meloman.kz with the parser's User-Agent. robots.txt forbids
`/catalogsearch/` to all robots, so there is no search page here; catalog pages (`/books/?p=N`) and product
pages are allowed. Tests read the files offline; when Meloman changes its markup, save fresh pages and update
`MelomanSelectors`.

Saved pages are reduced to keep the repository small: product pages without `<style>`, `<svg>` and scripts
other than JSON-LD; the catalog page keeps only its product list and pager. Everything kept is unchanged.

| File | Source | Case |
|---|---|---|
| `listing-books-page-1.html` | `/books/` | catalog page, 50 products (in stock only) |
| `product-rouzvud-hardcover.html` | `/books/reading-memoirs-biographies/tru-krajm/rouzvud-dzh-…html` | Book JSON-LD, ISBN; JSON-LD price (11 900, loyalty card) differs from the price box (13 850) |
| `product-karamazovy-no-isbn.html` | `/russkaya-klassika-18725/dostoevskiy-f-bratya-karamazovy-9.html` | Product JSON-LD, no ISBN in the characteristics |
| `product-ford.html` | `/biographies/moja-zhizn-moi-dostizhenija.html` | regular product |
| `product-ford-discount-edited.html` | copy of `product-ford.html` | **edited**: Magento's standard old-price block (`data-price-type="oldPrice"`, 5 990) added to the price box |
| `product-ford-out-of-stock-edited.html` | copy of `product-ford.html` | **edited**: JSON-LD `availability` set to `OutOfStock`, `stock available` → `stock unavailable` |

No book on the checked pages had a discount or was out of stock (the catalog lists only products in stock),
so those two cases are edited copies of a real page rather than more requests to the store.
