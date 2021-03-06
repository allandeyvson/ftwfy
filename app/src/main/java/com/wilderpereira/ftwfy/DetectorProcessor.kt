/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wilderpereira.ftwfy

import android.util.SparseArray
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.text.Text
import com.google.android.gms.vision.text.TextBlock
import com.wilderpereira.ftwfy.camera.GraphicOverlay

/**
 * A very simple Processor which gets detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 * TODO: Make this implement Detector.Processor<TextBlock> and add text to the GraphicOverlay
</TextBlock> */
class DetectorProcessor internal constructor(private val mGraphicOverlay: GraphicOverlay<OcrGraphic>, private val textToFindParam: String) : Detector.Processor<TextBlock> {

    var textToFind = textToFindParam
    var regexEnabled = false

    override fun release() {
        mGraphicOverlay.clear()
    }

    override fun receiveDetections(detections: Detector.Detections<TextBlock>) {
        mGraphicOverlay.clear()

        val items = detections.detectedItems

        if (!regexEnabled && textToFind.contains("\\s+".toRegex())) {
            processPhrase(items)
        } else {
            processWords(items)
        }
    }

    private fun processPhrase(items: SparseArray<TextBlock>) {
        for (i in 0 until items.size()) {
            if (items.get(i) != null) {
                val components = items.get(i).components
                components
                        .filter { textToFind.isNotBlank() && it.value.contains(textToFind) }
                        .map {
                            val graphic = OcrGraphic(mGraphicOverlay, listOf(it), textToFind, true)
                            mGraphicOverlay.add(graphic)
                        }
            }
        }
    }

    private fun processWords(items: SparseArray<TextBlock>) {

        for (i in 0 until items.size()) {
            val item = items.valueAt(i)

            if (shouldProcessDetection(item)) {
                val lines: List<Text> = item.components as List<Text>
                val matchedTexts: ArrayList<Text>? = ArrayList()

                //for each of the found lines get each word
                lines.flatMap { it.components as List<Text> }
                        //filter only the desired text and add to the list of words that will be drawn
                        .filter {
                            if (regexEnabled) {
                                it.value.matches(textToFind.toRegex())
                            } else {
                                it.value == textToFind
                            }
                        }
                        .map { matchedTexts?.add(it) }

                val graphic = OcrGraphic(mGraphicOverlay, matchedTexts, textToFind, false)
                mGraphicOverlay.add(graphic)
            }
        }
    }

    /**
     * Checks if there was a detection and if it has matched
     * the regex when the regex is enabled or if it contains
     * the text if regex is not enabled
     */
    private fun shouldProcessDetection(item: TextBlock) : Boolean {
        if (item.value != null) {
            return if(regexEnabled) {
                item.value.contains(textToFind.toRegex())
            } else {
                item.value.contains(textToFind)
            }
        }
        return false
    }

}
