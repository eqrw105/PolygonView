package com.nims.polygonsview

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    val values = listOf(
        PolygonGraphView.ValueHolder("일번", 0.4f, Color.BLACK),
        PolygonGraphView.ValueHolder("이번", 0.8f, Color.BLACK),
        PolygonGraphView.ValueHolder("삼번", 0.2f, Color.BLACK),
        PolygonGraphView.ValueHolder("사번", 0.8f, Color.BLACK),
        PolygonGraphView.ValueHolder("오번", 0.9f, Color.BLACK)
    )

    val adapter = object : PolygonGraphView.Adapter<PolygonGraphView.ValueHolder>() {

        override fun getSize(): Int = values.size

        override fun onCreateValueHolder(pos: Int): PolygonGraphView.ValueHolder = values[pos]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val graph = findViewById<PolygonGraphView>(R.id.p)

        graph.setAdapter(adapter)
    }


}