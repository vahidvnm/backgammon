package com.arena.backgammon.core
import org.junit.Assert.*
import org.junit.Test
class GameEngineTest {
 @Test fun initialPositionHasFifteenEach(){val p=Position();assertEquals(15,p.points.sumOf{it.coerceAtLeast(0)});assertEquals(15,p.points.sumOf{(-it).coerceAtLeast(0)})}
 @Test fun doublesProvideFourMoves(){assertEquals(listOf(5,5,5,5),GameEngine.rollValues(5,5))}
 @Test fun barHasPriority(){val p=Position(barWhite=1);assertTrue(GameEngine.rawMoves(p,1).all{it.from==Move.BAR})}
 @Test fun blockedEntryHasNoMove(){val b=Position().points.copyOf().apply{this[23]=-2};val p=Position(points=b,barWhite=1);assertTrue(GameEngine.rawMoves(p,1).isEmpty())}
 @Test fun hitMovesBlotToBar(){val b=IntArray(24);b[6]=1;b[5]=-1;val p=GameEngine.apply(Position(points=b),Move(6,5,1));assertEquals(1,p.barBlack);assertEquals(1,p.points[5])}
 @Test fun maximumDiceRuleUsesBothWhenPossible(){val b=IntArray(24);b[5]=1;val moves=GameEngine.legalMoves(Position(points=b,offWhite=14),listOf(1,2));assertTrue(moves.isNotEmpty());assertTrue(GameEngine.sequences(Position(points=b,offWhite=14),listOf(1,2)).all{it.size==2||it.size==1})}
 @Test fun exactBearOff(){val b=IntArray(24);b[0]=1;val p=Position(points=b,offWhite=14);assertTrue(Move(0,Move.OFF,1) in GameEngine.rawMoves(p,1))}
}
