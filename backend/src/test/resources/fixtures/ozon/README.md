# Ozon fixtures

Pages saved on 2026-10-22 from https://ozon.kz with the parser's User-Agent (plain HTTP, no browser).
robots.txt forbids `/search` to all robots, so there is no search page here; category pages with `?page=`
are explicitly allowed, product pages are allowed. Tests read the files offline; when Ozon changes its pages,
save fresh ones and update `OzonSelectors`.

Ozon pages weigh 1.3–1.7 MB, almost all of it scripts and markup the parser never reads. The saved files keep
only the blocks the parser reads, unchanged: the JSON-LD scripts and the state `<div>`s of the widgets
`webPrice`, `webProductHeading`, `webCharacteristics`, `webShortCharacteristics`, `webSale` (product pages)
and `tileGridDesktop` (category page).

| File | Source | Case |
|---|---|---|
| `category-knigi-page-1.html` | `/category/knigi-16500/` | book category, 48 tiles (with and without crossed-out prices, with and without author) |
| `product-273872197-discount.html` | `/product/100-i-1-stihotvorenie-…-273872197/` | discount, several ISBNs, several authors |
| `product-5835994396.html` | `/product/oge-2027-geografiya-32-varianta-5835994396/` | no discount, no author in the title |
| `product-313345021-no-characteristics.html` | `/product/zelenaya-milya-king-stiven-313345021/` | characteristics are loaded later by script: no ISBN, year or cover type on the page |
| `product-5835994396-out-of-stock-edited.html` | copy of `product-5835994396.html` | **edited**: `isAvailable` set to `false`, JSON-LD `availability` to `OutOfStock` |

Every product looked at was in stock, so the out-of-stock case is an edited copy of a real page rather than
more requests to the store.
