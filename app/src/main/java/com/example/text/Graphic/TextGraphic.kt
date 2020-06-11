package com.example.text.Graphic

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.firebase.ml.vision.text.FirebaseVisionText

class TextGraphic internal constructor(overlay: GraphicOverlay,
                                       private val text: FirebaseVisionText.Element?): GraphicOverlay.Graphic(overlay){
    private val rectPaint: Paint = Paint()
    private val textPaint: Paint

    companion object {
        private val TEXT_COLOR = Color.BLUE
        private val TEXT_SIZE = 54.0f
        private val STROKE_WIDTH = 4.0f
    }


    init {
        rectPaint.color = TEXT_COLOR
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = STROKE_WIDTH

        textPaint = Paint()
        textPaint.color = TEXT_COLOR
        textPaint.textSize = TEXT_SIZE

    }


    override fun draw(canvas: Canvas?) {
        if (text == null) {
            throw IllegalStateException("Attempting to draw a null text.")
        }

        val rect = RectF(text.boundingBox)
        rect.left = translateX(rect.left)
        rect.right = translateX(rect.right)
        rect.top = translateY(rect.top)
        rect.bottom = translateY(rect.bottom)




        if (canvas != null) {
            canvas.drawText(text.text,rect.left,rect.bottom,textPaint)
            canvas.drawRect(rect, rectPaint);
        }


    }
}