## AI Ad Creative Generator

Small end-to-end web application that generates ad creatives (headline, CTA, survey question) using AI APIs and persists them in MySQL. Features multiple ad suggestions and automatic image generation using Google Gemini.

### Tech Stack

- **Backend**: Java 17, Spring Boot (Web, Data JPA, WebFlux)
- **Database**: MySQL
- **Frontend**: Single `index.html` with vanilla JavaScript (no frameworks)
- **AI**: Groq API for text generation (`gpt-oss-120b`), Google Gemini API for image generation

### Running Locally

1. **Prerequisites**
   - Java 17+ (must be set as default Java version)
   - Maven 3.6+
   - MySQL running locally
   - Groq API key (for text generation)
   - Google Gemini API key (for image generation)

   **Java 17 Setup**: Make sure your system uses Java 17 by default:
   ```bash
   java -version  # Should show Java 17
   ```

2. **Configure MySQL**

   Create a database (or let Hibernate create it):

   ```sql
   CREATE DATABASE ai_ad_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

   Update `src/main/resources/application.properties`:

   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/ai_ad_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
   spring.datasource.username=YOUR_USER
   spring.datasource.password=YOUR_PASSWORD
   ```

   **Note**: The database schema will be created/updated automatically when you first run the application. The `image_url` column uses `LONGTEXT` to accommodate large base64-encoded images.

   If you need to manually update an existing database:

   ```sql
   ALTER TABLE ad_creatives MODIFY COLUMN image_url LONGTEXT;
   ```

3. **Set API Keys**

   On your shell (example for bash):

   ```bash
   export GROQ_API_KEY="gsk-..."
   export GEMINI_API_KEY="AIzaSy..."
   ```

   **Important**: Make sure there are no spaces around the `=` sign and no extra whitespace in the key values.

4. **Build and Run**

   ```bash
   mvn spring-boot:run
   ```

5. **Use the App**

   - Open `http://localhost:8081/` in your browser.
   - Enter a campaign description and click **Generate Ad**.
   - The app will:
     - Generate 3 different ad suggestions and display them as cards (not saved yet)
     - When you click "Use this ad" on any suggestion:
       - That specific ad gets saved to the database
       - The app automatically calls Google Gemini to generate an advertising image
       - The image is displayed and stored with the saved ad
   - **Search Functionality**:
     - Use the search box to find specific ads from your database
     - Search by any keyword from prompts, headlines, CTAs, or survey questions
     - Press Enter or click "Search" to filter results
     - Click "Clear" to return to showing the last 5 ads
   - The page shows the last 5 generated ads with their images by default.

### API Endpoints

- **POST `/api/ad/generate`**
  - Request body:

    ```json
    {
      "prompt": "A new eco-friendly water bottle..."
    }
    ```

  - Response body: Returns one generated ad object (not saved to database)

    ```json
    {
      "id": null,
      "prompt": "...",
      "headline": "...",
      "cta": "...",
      "surveyQuestion": "...",
      "createdAt": null
    }
    ```

- **POST `/api/ad/save`**
  - Request body: Ad data to save

    ```json
    {
      "prompt": "...",
      "headline": "...",
      "cta": "...",
      "surveyQuestion": "...",
      "imageUrl": "..."
    }
    ```

  - Response body: Saved ad with ID and timestamp

- **GET `/api/ad/history`**
  - Returns an array of up to the last 5 ads (most recent first), including `imageUrl` if images were generated.

- **GET `/api/ad/search?q={searchTerm}`**
  - Search ads by prompt, headline, CTA, or survey question
  - Returns matching ads ordered by creation date (most recent first)
  - If no search term provided, returns the last 5 ads

- **POST `/api/ad/image`**
  - Request body:

    ```json
    {
      "adId": 1,
      "prompt": "Generate image for..."
    }
    ```

  - Response body:

    ```json
    {
      "imageUrl": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
    }
    ```

### Key Features

- **Multiple Suggestions**: Each "Generate Ad" click produces 3 different ad variations for user selection (only selected ones are saved)
- **Selective Saving**: Only ads you click "Use this ad" on get stored in the database
- **Automatic Image Generation**: Selected ads automatically get advertising images via Google Gemini API
- **Fallback Image Generation**: If Gemini API fails, a placeholder image is automatically generated
- **Image Storage**: Generated images are stored as base64 data URLs in the database
- **Search Functionality**: Search through all saved ads by prompt, headline, CTA, or survey question
- **History Display**: Last 5 saved ads shown with their associated images, or search results

### Troubleshooting

- **"Data truncation: Data too long for column 'image_url'"**: Database schema updated to use `LONGTEXT`. Restart the app to apply schema changes.
- **"Exceeded limit on max bytes to buffer"**: This is fixed in the current code with increased buffer size (10MB).
- **Gemini API authentication errors**: Ensure `GEMINI_API_KEY` environment variable is set correctly without extra spaces.
- **Only selected ads are saved**: This is the intended behavior - suggestions are generated but only saved when you click "Use this ad".
- **Java version issues**: Run `java -version` to confirm you're using Java 17.
- **Port already in use**: Change `server.port` in `application.properties` if 8081 is occupied.

### Assumptions & Tradeoffs

- No authentication or authorization is implemented.
- Uses Groq API for text generation (faster than OpenAI) and Google Gemini for image generation.
- Fallback to placeholder images if Gemini API fails (for reliability).
- Only user-selected ads are saved to database (suggestions are generated but not persisted until chosen).
- Prompting is intentionally simple: the system prompt instructs the model to return a single JSON object.
- Response parsing is defensive: we trim to the first/last JSON braces and parse with Jackson; if required fields are missing, a 5xx error is raised.
- Image generation happens automatically when selecting an ad - no manual trigger needed.
- Images are stored as base64 data URLs in the database (simple but not optimized for large scale).


