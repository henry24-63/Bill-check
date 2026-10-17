# Bijli Bill & Utility Checker - Website

Landing page and admin panel for Bijli Bill & Utility Checker (Pakistan utilities).

## Running Locally

You can run the website locally using any static HTTP server (e.g., Python HTTP server or Live Server in VS Code):

```bash
cd website
python3 -m http.server 8080
```
Then open `http://localhost:8080` in your browser.

## Deploying on Vercel

1. Connect your repository to Vercel.
2. Set **Root Directory** to `website`.
3. Deploy! No build step is required (pure HTML/CSS/JS ES modules).

## Configuration

Supabase credentials are defined in `js/config.js`. Ensure your Supabase instance URL and publishable key are correctly set.
