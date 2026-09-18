package com.arena.backgammon.render

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.arena.backgammon.core.*
import com.arena.backgammon.data.*

/** Hardware-accelerated OpenGL surface. Touches are ray-cast through the gameplay camera onto the board plane. */
class Board3DView(context: Context) : GLSurfaceView(context) {
    val boardRenderer = BoardRenderer()
    var onSource: (Int)->Unit = {}
    var onMove: (Move)->Unit = {}
    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8,8,8,8,24,0)
        setRenderer(boardRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }
    fun update(position:Position, dice:List<Int>, selected:Int?, legal:List<Move>, theme:BoardTheme, pieces:PieceStyle, diceStyle:DiceStyle, rolling:Boolean) {
        boardRenderer.update(RenderSnapshot(position.copyDeep(),dice,selected,legal,theme,pieces,diceStyle,rolling))
    }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_UP) return true
        val hit=boardRenderer.boardHit(e.x,e.y) ?: return true
        val snap=boardRenderer.snapshot
        val destinations=snap.legal.filter{it.from==snap.selected}
        val target=when {
            hit.first > 6.65f -> Move.OFF
            kotlin.math.abs(hit.first)<.48f -> Move.BAR
            else -> boardRenderer.pointAt(hit.first,hit.second)
        }
        destinations.filter{it.to==target}.maxByOrNull{it.die}?.let(onMove) ?: run {
            val owns=target==Move.BAR && snap.position.bar(snap.position.turn)>0 || target in 0..23 && snap.position.points[target]*snap.position.turn.sign>0
            if(owns) onSource(target)
        }
        return true
    }
}

data class RenderSnapshot(val position:Position=Position(),val dice:List<Int> = emptyList(),val selected:Int?=null,val legal:List<Move> = emptyList(),val theme:BoardTheme=BoardTheme.CLASSIC_WOOD,val pieces:PieceStyle=PieceStyle.IVORY,val diceStyle:DiceStyle=DiceStyle.CLASSIC,val rolling:Boolean=false)
