package com.dara.backgammon.ai

import com.dara.backgammon.core.*
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
        val race=(theirs-mine).toDouble()/(mine+theirs)
        val borneOff=(position.off(me)-position.off(me.other()))*.045
        val bar=(position.bar(me.other())-position.bar(me))*.035
        val home=(homeStrength(position,me)-homeStrength(position,me.other()))*.006
        val structure=(longestPrime(position,me)-longestPrime(position,me.other()))*.012
        val exposure=(blots(position,me.other())-blots(position,me))*.006
        return (race+borneOff+bar+home+structure+exposure).coerceIn(-1.0,1.0)
    }
    fun shouldOfferDouble(position:Position,me:Player)=equity(position,me)>.13
    fun shouldTakeDouble(position:Position,taker:Player)=equity(position,taker)>-.27

    /** Match-aware cube decisions. Crawford and cube ownership are still enforced by the match controller. */
    fun shouldOfferDouble(position:Position,me:Player,match:MatchInfo):Boolean {
        if(match.crawfordGame||match.cube>=64)return false
        val myScore=if(me==Player.WHITE)match.whiteScore else match.blackScore
        val theirScore=if(me==Player.WHITE)match.blackScore else match.whiteScore
        val myAway=(match.target-myScore).coerceAtLeast(1)
        val theirAway=(match.target-theirScore).coerceAtLeast(1)
        val threshold=when {
            myAway==1 -> .20
            theirAway==1 -> .09
            myAway>theirAway -> .11
            myAway<theirAway -> .15
            else -> .13
        }
        return equity(position,me)>threshold
    }

    fun shouldTakeDouble(position:Position,taker:Player,match:MatchInfo):Boolean {
        val myScore=if(taker==Player.WHITE)match.whiteScore else match.blackScore
        val theirScore=if(taker==Player.WHITE)match.blackScore else match.whiteScore
        val myAway=(match.target-myScore).coerceAtLeast(1)
        val theirAway=(match.target-theirScore).coerceAtLeast(1)
        val threshold=when {
            myAway==1 -> -.18
            theirAway==1 -> -.34
            myAway>theirAway -> -.31
            myAway<theirAway -> -.22
            else -> -.27
        }
        return equity(position,taker)>threshold
    }

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
    private fun blots(p:Position,player:Player)=(0..23).count{p.points[it]*player.sign==1}
    private fun homeStrength(p:Position,player:Player):Int {
        val home=if(player==Player.WHITE)0..5 else 18..23
        return home.sumOf{i->when(val count=p.points[i]*player.sign){in 2..15->minOf(count,3);else->0}}
    }
    private fun longestPrime(p:Position,player:Player):Int {
        var best=0;var current=0
        for(i in if(player==Player.WHITE)23 downTo 0 else 0..23){
            if(p.points[i]*player.sign>=2){current++;best=maxOf(best,current)}else current=0
        }
        return best
    }
    private fun anchors(p:Position,player:Player):Int {
        val opponentHome=if(player==Player.WHITE)18..23 else 0..5
        return opponentHome.count{p.points[it]*player.sign>=2}
    }
    private fun stackWaste(p:Position,player:Player):Int=(0..23).sumOf{(p.points[it]*player.sign-3).coerceAtLeast(0)}
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
        val prime=longestPrime(p,me);val opponentPrime=longestPrime(p,me.other())
        score+=prime*prime*1.35-opponentPrime*opponentPrime*.75
        score+=anchors(p,me)*3.2-anchors(p,me.other())*2.1
        score+=homeStrength(p,me)*1.15-homeStrength(p,me.other())*.55
        score-=stackWaste(p,me)*1.8
        if(p.off(me)>0)score-=stackWaste(p,me)*1.4
        if(p.off(me)>0&&p.off(me.other())==0)score+=8.0
        return score
    }
    private fun replySafety(p:Position,me:Player):Double{
        val opponent=p.copy(turn=me.other());val baseline=evaluate(p,me,p);var replyDelta=0.0
        for(a in 1..6)for(b in a..6){
            val sequences=GameEngine.sequences(opponent,GameEngine.rollValues(a,b))
            val worst=sequences.minOfOrNull{evaluate(play(opponent,it),me,p)}?:baseline
            // Only the damage or benefit of the reply belongs here; adding the full board score
            // would count the same static position twice and distort Expert decisions.
            replyDelta+=(worst-baseline)*if(a==b)1.0 else 2.0
        }
        return replyDelta/36.0*weights(p,me).safety
    }
}
