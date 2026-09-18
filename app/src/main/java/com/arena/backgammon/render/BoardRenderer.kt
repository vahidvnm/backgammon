package com.arena.backgammon.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.arena.backgammon.core.*
import com.arena.backgammon.data.*
import java.nio.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

class BoardRenderer : GLSurfaceView.Renderer {
    @Volatile var snapshot=RenderSnapshot()
    private var program=0;private var width=1;private var height=1
    private val projection=FloatArray(16);private val view=FloatArray(16);private val vp=FloatArray(16)
    private val cube=Mesh.cube();private val cylinder=Mesh.cylinder(28);private val triangle=Mesh.triangle()
    private var start=System.nanoTime()
    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?){
        program=shader(VERTEX,FRAGMENT);GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glEnable(GLES20.GL_CULL_FACE);GLES20.glEnable(GLES20.GL_BLEND);GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }
    override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int){width=w;height=h;GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(projection,0,34f,w.toFloat()/h,1f,40f);Matrix.setLookAtM(view,0,0f,10.5f,11.7f,0f,0f,0f,0f,1f,0f);Matrix.multiplyMM(vp,0,projection,0,view,0)}
    override fun onDrawFrame(gl:GL10?){
        val s=snapshot;val t=(System.nanoTime()-start)/1e9f;val c=ThemeColors.of(s.theme)
        GLES20.glClearColor(c.environment[0],c.environment[1],c.environment[2],1f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT);GLES20.glUseProgram(program)
        // table shadow, solid frame, inset felt bed and central bar
        draw(cube,0f,-.25f,0f,15.2f,.35f,9.4f,c.shadow,0f)
        draw(cube,0f,.05f,0f,14.7f,.72f,8.8f,c.wood,0f)
        draw(cube,0f,.43f,0f,13.55f,.12f,7.65f,c.felt,0f)
        draw(cube,0f,.59f,0f,.72f,.28f,7.7f,c.woodDark,0f)
        draw(cube,6.98f,.5f,0f,.48f,.42f,7.8f,c.metal,0f)
        // alternating inlaid points as actual raised triangular meshes
        for(i in 0..11){val x=columnX(i);val a=if(i%2==0)c.pointA else c.pointB;val b=if(i%2==0)c.pointB else c.pointA;draw(triangle,x,.58f,-3.72f,.98f,.05f,3.0f,a,0f);draw(triangle,x,.58f,3.72f,.98f,.05f,3.0f,b,180f)}
        // hinges and gold detailing
        draw(cube,0f,.78f,-2.0f,.88f,.14f,.58f,c.metal,0f);draw(cube,0f,.78f,2.0f,.88f,.14f,.58f,c.metal,0f)
        val p=s.position
        for(point in 0..23){val count=abs(p.points[point]);for(i in 0 until count){val pos=checkerPosition(point,i,count);val selected=s.selected==point&&i==count-1;val lift=if(selected).28f+.05f*sin(t*5) else 0f;draw(cylinder,pos.first,.74f+lift,pos.second,.78f,.22f,.78f,pieceColor(p.points[point]>0,s.pieces,c),0f,if(selected)c.glow else null)}}
        if(p.barWhite>0)draw(cylinder,-.02f,.9f,.72f,.76f,.22f,.76f,pieceColor(true,s.pieces,c),0f,if(s.selected==Move.BAR)c.glow else null)
        if(p.barBlack>0)draw(cylinder,.02f,.9f,-.72f,.76f,.22f,.76f,pieceColor(false,s.pieces,c),0f,if(s.selected==Move.BAR)c.glow else null)
        // legal landing halos
        s.legal.filter{it.from==s.selected}.forEach{m->val q=if(m.to==Move.OFF)6.98f to 0f else checkerPosition(m.to,abs(p.points[m.to]).coerceAtMost(5),6);draw(cylinder,q.first,.66f,q.second,.88f,.025f,.88f,c.glow.copyOf().apply{this[3]=.48f},0f)}
        s.dice.take(2).forEachIndexed{i,d->val rolling=s.rolling;val angle=if(rolling)t*420f+i*71 else dieAngle(d);drawDie(if(i==0)-1.05f else 1.05f,1.05f,0f,d,angle,c,s.diceStyle)}
    }
    fun pointAt(x:Float,z:Float):Int{val col=(0..11).minBy{abs(columnX(it)-x)};return if(z<0)11-col else 12+col}
    fun boardHit(px:Float,py:Float):Pair<Float,Float>?{
        val inv=FloatArray(16);if(!Matrix.invertM(inv,0,vp,0))return null
        fun unproject(z:Float):FloatArray{val v=floatArrayOf(px/width*2-1,1-py/height*2,z,1f);val o=FloatArray(4);Matrix.multiplyMV(o,0,inv,0,v,0);return floatArrayOf(o[0]/o[3],o[1]/o[3],o[2]/o[3])}
        val near=unproject(-1f);val far=unproject(1f);val dy=far[1]-near[1];if(abs(dy)<.001f)return null;val k=(.62f-near[1])/dy;val x=near[0]+(far[0]-near[0])*k;val z=near[2]+(far[2]-near[2])*k;return if(x in -7.5f..7.5f&&z in -4.6f..4.6f)x to z else null
    }
    private fun columnX(col:Int):Float{val raw=-5.85f+col*1.065f;return raw+if(col>=6).38f else -.38f}
    private fun checkerPosition(point:Int,index:Int,count:Int):Pair<Float,Float>{val col=if(point<12)11-point else point-12;val x=columnX(col);val top=point>=12;val spacing=if(count>5)min(.66f,2.8f/(count-1))else .66f;val z=if(top)3.28f-index*spacing else -3.28f+index*spacing;return x to z}
    private fun pieceColor(white:Boolean,style:PieceStyle,c:ThemeColors)=when(style){PieceStyle.IVORY->if(white)c.white else c.black;PieceStyle.MARBLE->if(white)floatArrayOf(.72f,.8f,.86f,1f) else floatArrayOf(.12f,.2f,.3f,1f);PieceStyle.NEON->if(white)floatArrayOf(.2f,.82f,1f,1f) else floatArrayOf(1f,.15f,.38f,1f)}
    private fun drawDie(x:Float,y:Float,z:Float,value:Int,angle:Float,c:ThemeColors,style:DiceStyle){val color=when(style){DiceStyle.CLASSIC->c.white;DiceStyle.ONYX->c.black;DiceStyle.CRYSTAL->c.glow};draw(cube,x,y,z,.82f,.82f,.82f,color,angle); // recessed-looking pip discs on visible face
        val pip=if(style==DiceStyle.CLASSIC)c.black else c.white;pipLayout(value).forEach{(a,b)->draw(cylinder,x+a*.48f,y+.425f,z+b*.48f,.095f,.018f,.095f,pip,0f)}}
    private fun pipLayout(v:Int)=when(v){1->listOf(0f to 0f);2->listOf(-.45f to -.45f,.45f to .45f);3->listOf(-.48f to -.48f,0f to 0f,.48f to .48f);4->listOf(-.45f to -.45f,.45f to -.45f,-.45f to .45f,.45f to .45f);5->listOf(-.48f to -.48f,.48f to -.48f,0f to 0f,-.48f to .48f,.48f to .48f);else->listOf(-.45f to -.55f,.45f to -.55f,-.45f to 0f,.45f to 0f,-.45f to .55f,.45f to .55f)}
    private fun dieAngle(v:Int)=floatArrayOf(0f,90f,180f,270f,45f,135f)[v-1]
    private fun draw(mesh:Mesh,x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,color:FloatArray,rotation:Float,emission:FloatArray?=null){val m=FloatArray(16);val mvp=FloatArray(16);Matrix.setIdentityM(m,0);Matrix.translateM(m,0,x,y,z);Matrix.rotateM(m,0,rotation,1f,1f,0f);Matrix.scaleM(m,0,sx,sy,sz);Matrix.multiplyMM(mvp,0,vp,0,m,0);mesh.draw(program,m,mvp,emission?:color,color)}
    private fun shader(v:String,f:String):Int{
        fun compile(type:Int,source:String):Int {
            val id=GLES20.glCreateShader(type)
            GLES20.glShaderSource(id,source)
            GLES20.glCompileShader(id)
            return id
        }
        val id=GLES20.glCreateProgram()
        GLES20.glAttachShader(id,compile(GLES20.GL_VERTEX_SHADER,v))
        GLES20.glAttachShader(id,compile(GLES20.GL_FRAGMENT_SHADER,f))
        GLES20.glLinkProgram(id)
        return id
    }
    companion object {const val VERTEX="attribute vec3 aPos;attribute vec3 aNormal;uniform mat4 uM;uniform mat4 uMvp;varying vec3 n;varying vec3 world;void main(){world=(uM*vec4(aPos,1.)).xyz;n=normalize(mat3(uM)*aNormal);gl_Position=uMvp*vec4(aPos,1.);}";const val FRAGMENT="precision mediump float;uniform vec4 uColor;uniform vec4 uEmission;varying vec3 n;varying vec3 world;void main(){vec3 l=normalize(vec3(-.4,1.,.55));float d=max(dot(normalize(n),l),0.);float rim=pow(1.-max(dot(normalize(n),normalize(vec3(0.,1.,1.))),0.),3.);vec3 col=uColor.rgb*(.30+d*.70)+uEmission.rgb*rim*.16;gl_FragColor=vec4(col,uColor.a);}"}
}

class Mesh(private val vertices:FloatArray){private val buffer=ByteBuffer.allocateDirect(vertices.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(vertices);position(0)};fun draw(program:Int,m:FloatArray,mvp:FloatArray,emission:FloatArray,color:FloatArray){val pos=GLES20.glGetAttribLocation(program,"aPos");val normal=GLES20.glGetAttribLocation(program,"aNormal");buffer.position(0);GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,24,buffer);buffer.position(3);GLES20.glVertexAttribPointer(normal,3,GLES20.GL_FLOAT,false,24,buffer);GLES20.glEnableVertexAttribArray(pos);GLES20.glEnableVertexAttribArray(normal);GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program,"uM"),1,false,m,0);GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program,"uMvp"),1,false,mvp,0);GLES20.glUniform4fv(GLES20.glGetUniformLocation(program,"uColor"),1,color,0);GLES20.glUniform4fv(GLES20.glGetUniformLocation(program,"uEmission"),1,emission,0);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertices.size/6)}
 companion object {fun cube():Mesh{val v=mutableListOf<Float>();fun face(a:FloatArray,b:FloatArray,c:FloatArray,d:FloatArray,n:FloatArray){listOf(a,b,c,a,c,d).forEach{v+=it.toList();v+=n.toList()}};face(floatArrayOf(-.5f,.5f,-.5f),floatArrayOf(.5f,.5f,-.5f),floatArrayOf(.5f,.5f,.5f),floatArrayOf(-.5f,.5f,.5f),floatArrayOf(0f,1f,0f));face(floatArrayOf(-.5f,-.5f,.5f),floatArrayOf(.5f,-.5f,.5f),floatArrayOf(.5f,-.5f,-.5f),floatArrayOf(-.5f,-.5f,-.5f),floatArrayOf(0f,-1f,0f));face(floatArrayOf(-.5f,-.5f,.5f),floatArrayOf(-.5f,.5f,.5f),floatArrayOf(.5f,.5f,.5f),floatArrayOf(.5f,-.5f,.5f),floatArrayOf(0f,0f,1f));face(floatArrayOf(.5f,-.5f,-.5f),floatArrayOf(.5f,.5f,-.5f),floatArrayOf(-.5f,.5f,-.5f),floatArrayOf(-.5f,-.5f,-.5f),floatArrayOf(0f,0f,-1f));face(floatArrayOf(-.5f,-.5f,-.5f),floatArrayOf(-.5f,.5f,-.5f),floatArrayOf(-.5f,.5f,.5f),floatArrayOf(-.5f,-.5f,.5f),floatArrayOf(-1f,0f,0f));face(floatArrayOf(.5f,-.5f,.5f),floatArrayOf(.5f,.5f,.5f),floatArrayOf(.5f,.5f,-.5f),floatArrayOf(.5f,-.5f,-.5f),floatArrayOf(1f,0f,0f));return Mesh(v.toFloatArray())}
 fun cylinder(segments:Int):Mesh{val v=mutableListOf<Float>();fun add(x:Float,y:Float,z:Float,nx:Float,ny:Float,nz:Float){v+=listOf(x,y,z,nx,ny,nz)};for(i in 0 until segments){val a=i*2*PI/segments;val b=(i+1)*2*PI/segments;val ax=cos(a).toFloat()*.5f;val az=sin(a).toFloat()*.5f;val bx=cos(b).toFloat()*.5f;val bz=sin(b).toFloat()*.5f;add(0f,.5f,0f,0f,1f,0f);add(ax,.5f,az,0f,1f,0f);add(bx,.5f,bz,0f,1f,0f);add(ax,-.5f,az,ax*2,0f,az*2);add(ax,.5f,az,ax*2,0f,az*2);add(bx,.5f,bz,bx*2,0f,bz*2);add(ax,-.5f,az,ax*2,0f,az*2);add(bx,.5f,bz,bx*2,0f,bz*2);add(bx,-.5f,bz,bx*2,0f,bz*2)};return Mesh(v.toFloatArray())}
 fun triangle()=Mesh(floatArrayOf(-.5f,0f,0f,0f,1f,0f,.5f,0f,0f,0f,1f,0f,0f,0f,1f,0f,1f,0f))}}
