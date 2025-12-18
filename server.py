import os
from io import BytesIO
from base64 import b64encode

import google.generativeai as genai
from flask import Flask, request, jsonify

# Configure Gemini API key (use env var or fallback to provided key)
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY") 
genai.configure(api_key=GEMINI_API_KEY)

# Initialize the model for image generation
model = genai.GenerativeModel(
    "gemini-2.5-flash-image",  # Fast generation
    generation_config={
        "response_modalities": ["TEXT", "IMAGE"],
    }
)

app = Flask(__name__)

@app.post("/generate-image")
def generate_image():
    data = request.get_json(force=True) or {}
    prompt = data.get("prompt", "").strip()
    if not prompt:
        return jsonify({"error": "prompt is required"}), 400

    try:
        # Generate content with Gemini
        response = model.generate_content(prompt)
        
        # Find the image in the response
        image_data = None
        for part in response.parts:
            if part.inline_data:
                image_data = part.inline_data.data
                break
        
        if not image_data:
            return jsonify({"error": "No image generated"}), 500
        
        # Convert to base64
        b64 = b64encode(image_data).decode("utf-8")
        return jsonify({"imageBase64": b64})
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001)