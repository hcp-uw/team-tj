"""
VerifAI backend entry point.
Run with: python server.py
"""
if __name__ == "__main__":
    import sys
    import os
    # Ensure the backend directory is on the path so `app` imports work
    sys.path.insert(0, os.path.dirname(__file__))
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8080, reload=False)
