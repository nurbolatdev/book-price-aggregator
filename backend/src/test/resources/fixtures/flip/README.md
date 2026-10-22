# Flip.kz fixtures

Pages saved on 2026-10-22 from https://www.flip.kz with the parser's User-Agent (robots.txt allows these paths).
Tests read them offline; when Flip changes its markup, save fresh pages and update `FlipSelectors`.

| File | Source | Case |
|---|---|---|
| `search-master-i-margarita.html` | `/search?search=мастер и маргарита` | search results, 60 cards |
| `product-1269662.html` | `/catalog?prod=1269662` | regular product, no discount |
| `product-295466-discount.html` | `/catalog?prod=295466` | discount (old price shown) |
| `product-103811-several-isbn.html` | `/catalog?prod=103811` | several ISBNs in one cell |
| `product-out-of-stock-edited.html` | copy of `product-1269662.html` | **edited**: JSON-LD `availability` set to `OutOfStock`, `data-available="0"` |
| `product-no-isbn-edited.html` | copy of `product-1269662.html` | **edited**: the ISBN row of the characteristics table removed |

Flip's search shows only products in stock, and every product checked had an ISBN, so the last two cases
are edited copies of a real page rather than additional requests to the store.
