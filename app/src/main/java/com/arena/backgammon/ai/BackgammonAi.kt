package com.arena.backgammon.ai

import com.arena.backgammon.core.*
import kotlin.math.abs
import kotlin.random.Random

enum class Difficulty { BEGINNER, EASY, MEDIUM, HARD, EXPERT }
enum class AiPersona(val title:String,val description:String) {
    BUILDER("THE BUILDER","Constructs points and long defensive primes"),
    RUNNER("THE RUNNER","Races efficiently and protects its pip lead"),
    HUNTER("THE HUNTER","Attacks blots and keeps checkers on the bar"),
    MASTER("THE MASTER","Balances racing, structure, safety and replies")
}

private data class StyleWeights(
    val race:Double,val madePoint:Double,val homePoint:Double,
    val blot:Double,val bar:Double,val hit:Double,val safety:Double
)

object BackgammonAi {
    fun chooseSequence(position:Position,dice:List<Int>,level:Difficulty,persona:AiPersona=AiPersona.MASTER,random:Random=Random.Default):List<Move>{
        val choices=GameEngine.sequences(position,dice).filter{it.isNotEmpty()}
        if(choices.isEmpty())return emptyList()
        if(level==Difficulty.BEGINNER)return choices.random(random)
        val scored=choices.map{seq->seq to evaluate(play(position,seq),position.turn,position)}.sortedByDescending{it.second}
        return when(level){
            Difficulty.EASY->scored.take(minOf(4,scored.size)).random(random).first
            Difficulty.MEDIUM->scored.take(minOf(2,scored.size)).random(random).first
            Difficulty.HARD->scored.first().first
            Difficulty.EXPERT->scored.maxBy{(seq,score)->score+replySafety(play(position,seq),position.turn)}.first
            else->choices.first()
        }
    }

    /** Lightweight cube intelligence: positive values favour [me]. */
    fun equity(position:Position,me:Player):Double{
        val mine=pip(position,me).coerceAtLeast(1);val theirs=pip(position,me.other()).coerceAtLeast(1)
        return (theirs-mine).toDouble()/(mine+theirs)+(position.off(me)-position.off(me.other()))*.045+(position.bar(me.other())-position.bar(me))*.035
    }
    fun shouldOfferDouble(position:Position,me:Player)=equity(position,me)>.13
    fun shouldTakeDouble(position:Position,taker:Player)=equity(position,taker)>-.27

    /** Chooses strategy from the position itself instead of forcing a personality. */
    private fun weights(p:Position,me:Player):StyleWeights {
        val myPip=pip(p,me);val theirPip=pip(p,me.other());val race=(0..23).none{i->p.points[i]*me.sign>0&&(0..23).any{j->p.points[j]*me.other().sign>0&&if(me==Player.WHITE)j<i else j>i}}
        val bearing=p.off(me)>0||p.off(me.other())>0
        val pressure=p.bar(me.other())>0
        val made=(0..23).count{p.points[it]*me.sign>=2}
        return when {
            race||bearing->StyleWeights(1.82,2.4,1.5,5.8,13.0,7.0,.34)
            pressure->StyleWeights(1.02,4.0,3.0,2.0,29.0,31.0,.20)
            made<4->StyleWeights(1.08,7.0,4.0,2.8,18.0,15.0,.24)
            myPip+12<theirPip->StyleWeights(1.55,3.3,2.0,4.6,17.0,11.0,.31)
            else->StyleWeights(1.26,4.8,2.8,3.2,22.0,20.0,.28)
        }
    }
    private fun pip(p:Position,player:Player)=(0..23).sumOf{i->
        val count=(p.points[i]*player.sign).coerceAtLeast(0)
        count*if(player==Player.WHITE)i+1 else 24-i
    }+p.bar(player)*25
    private fun play(start:Position,moves:List<Move>)=moves.fold(start){p,m->GameEngine.apply(p,m)}
    private fun evaluate(p:Position,me:Player,before:Position=p):Double{
        val w=weights(p,me)
        var score=(pip(p,me.other())-pip(p,me))*w.race+(p.off(me)-p.off(me.other()))*45
        for(i in 0..23){
            val mine=p.points[i]*me.sign
            if(mine>=2){score+=w.madePoint+minOf(mine,3);val home=if(me==Player.WHITE)i in 0..5 else i in 18..23;if(home)score+=w.homePoint}
            if(mine==1)score-=w.blot
            if(p.points[i]*me.other().sign==1&&abs(i-12)<10)score+=.45
        }
        val newlyHit=(p.bar(me.other())-before.bar(me.other())).coerceAtLeast(0)
        score+=p.bar(me.other())*w.bar-p.bar(me)*22+newlyHit*w.hit
        return score
    }
    private fun replySafety(p:Position,me:Player):Double{
        val opponent=p.copy(turn=me.other());var danger=0.0
        for(a in 1..6)for(b in a..6){
            val sequences=GameEngine.sequences(opponent,GameEngine.rollValues(a,b))
            val worst=sequences.minOfOrNull{evaluate(play(opponent,it),me,p)}?:evaluate(p,me,p)
            danger+=worst*if(a==b)1.0 else 2.0
        }
        return danger/36.0*weights(p,me).safety
    }
}
