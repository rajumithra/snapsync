import os
import io
import math
import logging
from typing import List, Dict, Any, Optional
import numpy as np
import cv2
from PIL import Image, ImageOps

logger = logging.getLogger("snapsync.face")

class FaceService:
    def __init__(self):
        self.embedding_dim = 128
        self.models_dir = os.path.abspath(
            os.path.join(os.path.dirname(__file__), "../ai_models")
        )
        self.yunet_path = os.path.join(self.models_dir, "face_detection_yunet_2023mar.onnx")
        self.sface_path = os.path.join(self.models_dir, "face_recognition_sface_2021dec.onnx")

        self.detector = None
        self.recognizer = None
        self._init_models()

    def _init_models(self):
        try:
            if os.path.exists(self.yunet_path) and os.path.exists(self.sface_path):
                self.detector = cv2.FaceDetectorYN.create(
                    model=self.yunet_path,
                    config="",
                    input_size=(320, 320),
                    score_threshold=0.60,
                    nms_threshold=0.30,
                    top_k=5000
                )
                self.recognizer = cv2.FaceRecognizerSF.create(
                    model=self.sface_path,
                    config=""
                )
                logger.info("Deep learning face models (YuNet + SFace) loaded successfully.")
            else:
                logger.warning(f"Face models not found at {self.models_dir}. Face recognition will use fallback.")
        except Exception as e:
            logger.error(f"Failed to initialize DNN face models: {e}")

    def decode_image(self, image_data: bytes) -> Optional[np.ndarray]:
        """
        Safely decodes image bytes into a BGR numpy image with correct EXIF orientation.
        """
        try:
            pil_img = Image.open(io.BytesIO(image_data))
            pil_img = ImageOps.exif_transpose(pil_img)
            if pil_img.mode != "RGB":
                pil_img = pil_img.convert("RGB")
            # Convert RGB PIL to BGR OpenCV
            rgb_arr = np.array(pil_img)
            bgr_arr = cv2.cvtColor(rgb_arr, cv2.COLOR_RGB2BGR)
            return bgr_arr
        except Exception as e:
            logger.error(f"Error decoding image: {e}")
            return None

    def detect_faces_and_embed(self, image_data: bytes) -> List[Dict[str, Any]]:
        """
        Detects real faces in an image using OpenCV YuNet and computes
        high-discrimination 128D deep feature embeddings via SFace.
        Returns empty list if no face is present.
        """
        img = self.decode_image(image_data)
        if img is None:
            return []

        h, w = img.shape[:2]
        if h == 0 or w == 0:
            return []

        if self.detector is not None and self.recognizer is not None:
            try:
                # Downscale large high-res camera photos (e.g. 24MP-60MP) for fast detection
                max_dim = 1600
                scale = 1.0
                if max(h, w) > max_dim:
                    scale = max_dim / float(max(h, w))
                    new_w = int(w * scale)
                    new_h = int(h * scale)
                    proc_img = cv2.resize(img, (new_w, new_h), interpolation=cv2.INTER_AREA)
                else:
                    proc_img = img

                ph, pw = proc_img.shape[:2]
                self.detector.setInputSize((pw, ph))
                _, detected_faces = self.detector.detect(proc_img)

                if detected_faces is None or len(detected_faces) == 0:
                    return []

                results = []
                for face in detected_faces:
                    score = float(face[-1])
                    if score < 0.60:
                        continue

                    # Align and extract deep feature embedding
                    aligned = self.recognizer.alignCrop(proc_img, face)
                    feat = self.recognizer.feature(aligned)
                    norm_feat = feat.flatten()

                    # L2 normalize
                    norm = np.linalg.norm(norm_feat)
                    if norm > 0:
                        norm_feat = norm_feat / norm

                    box_left = float(face[0] / pw)
                    box_top = float(face[1] / ph)
                    box_right = float((face[0] + face[2]) / pw)
                    box_bottom = float((face[1] + face[3]) / ph)

                    results.append({
                        "box_top": max(0.0, min(1.0, box_top)),
                        "box_left": max(0.0, min(1.0, box_left)),
                        "box_bottom": max(0.0, min(1.0, box_bottom)),
                        "box_right": max(0.0, min(1.0, box_right)),
                        "confidence": round(score, 3),
                        "embedding": norm_feat.tolist()
                    })

                return results
            except Exception as e:
                logger.error(f"DNN face detection error: {e}")
                return []

        # If models unavailable, return empty (never return false positive matches)
        return []

    def compute_similarity(self, embedding_a: List[float], embedding_b: List[float]) -> float:
        """
        Computes cosine similarity between two 128D deep face embeddings.
        Returns a value in [0.0, 1.0].
        Scores >= 0.65 represent the same person.
        Scores < 0.35 represent different people.
        """
        if not embedding_a or not embedding_b:
            return 0.0

        vec_a = np.array(embedding_a, dtype=np.float32).reshape(1, -1)
        vec_b = np.array(embedding_b, dtype=np.float32).reshape(1, -1)

        if self.recognizer is not None:
            try:
                sim = self.recognizer.match(vec_a, vec_b, dis_type=cv2.FaceRecognizerSF_FR_COSINE)
                return max(0.0, min(1.0, float(sim)))
            except Exception:
                pass

        # Manual cosine similarity fallback
        dot = float(np.dot(vec_a.flatten(), vec_b.flatten()))
        norm_a = float(np.linalg.norm(vec_a))
        norm_b = float(np.linalg.norm(vec_b))
        if norm_a == 0 or norm_b == 0:
            return 0.0
        cos = dot / (norm_a * norm_b)
        return max(0.0, min(1.0, cos))

face_service = FaceService()
