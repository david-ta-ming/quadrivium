# Examples

**This folder contains example magic squares of order 3 to 200.** Output is converted to [Frénicle standard form](https://en.wikipedia.org/wiki/Fr%C3%A9nicle_standard_form#:~:text=A%20magic%20square%20is%20in,in%20%5B2%2C1%5D.) to distinguish rotated and transposed duplicates.

## Filename Format

Files are named as `<order>_<first>-<second>-<third>_<hash>.txt` where:
- `<order>` is the size of the square (e.g., 3 for 3×3)
- `<first>-<second>-<third>` are the first three values in the top row
- `<hash>` is the first 16 characters of the MD5 hash of the square's flattened values

Example for the Lo Shu magic square:
```
2 7 6
9 5 1
4 3 8
```
Filename: `3_2-7-6_ac805218ca6a9d8b.txt`
