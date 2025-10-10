# Proposed Maintenance Tasks

## Fix a Typo
- **Location:** `src/main/java/com/diepnn/shortenurl/exception/GlobalExceptionHandler.java`
- **Issue:** The TooManyRequestException handler method is mistakenly named `handleNoHandlerFoundException`, which duplicates another handler's name and obscures the actual purpose of the method. 【F:src/main/java/com/diepnn/shortenurl/exception/GlobalExceptionHandler.java†L94-L103】
- **Proposed Task:** Rename the method to `handleTooManyRequestException` (and adjust any related references) so the handler name reflects the exception it processes.

## Fix a Bug
- **Location:** `src/main/resources/shorten_url.sql`
- **Issue:** The `url_info` table declares a unique index on the non-existent column `short_url`, which causes schema creation to fail. 【F:src/main/resources/shorten_url.sql†L41-L55】
- **Proposed Task:** Update the index definition to reference the existing `short_code` column so migrations succeed.

## Correct Documentation / Comment Discrepancy
- **Location:** `src/main/java/com/diepnn/shortenurl/controller/UrlInfoController.java`
- **Issue:** The Javadoc for `create(...)` claims `TooManyRequestException` is thrown when "the number of short URLs created exceeds the limit," but in reality the exception bubbles up from the ID generator when it rate-limits ID allocation. 【F:src/main/java/com/diepnn/shortenurl/controller/UrlInfoController.java†L48-L77】
- **Proposed Task:** Reword the documentation to describe the real trigger (ID generation rate limiting) so client developers receive accurate guidance.

## Improve a Test
- **Location:** `src/test/java/com/diepnn/shortenurl/service/ResolveUrlServiceImplTests.java`
- **Issue:** The test `resolve_whenUrlInfoExists_returnUrlInfo` never asserts the URL returned by `resolve(...)`, despite its name promising that behavior. 【F:src/test/java/com/diepnn/shortenurl/service/ResolveUrlServiceImplTests.java†L45-L60】
- **Proposed Task:** Stub the cached URL to include a known `originalUrl` value and assert that `ResolveUrlServiceImpl.resolve(...)` returns it, in addition to the existing interaction verifications.
