# AI Models for Video Enhancement

This directory contains TensorFlow Lite models for video enhancement features:

## Required Models

1. **upscaling_model.tflite** - AI upscaling model (720p → 1080p, 1080p → 4K)
2. **hdr_model.tflite** - HDR tone mapping model
3. **denoise_model.tflite** - Noise reduction model

## Model Requirements

- Input format: RGB888 or YUV420
- GPU acceleration compatible
- Quantized for mobile performance
- Models should be < 50MB each for optimal loading

## Installation

Place the .tflite model files in this directory. The SmartVideoEnhancementEngine will automatically load them at runtime.

## Notes

- Fallback CPU processing is available if models are missing
- Models are loaded on-demand to save memory
- GPU acceleration is used when available