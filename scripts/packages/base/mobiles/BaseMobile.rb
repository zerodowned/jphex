#-------------------------------------------------------------------------------
# Copyright (c) 2013 Folke Will <folke.will@gmail.com>
# 
# This file is part of JPhex.
# 
# JPhex is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# 
# JPhex is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#-------------------------------------------------------------------------------
class BaseMobile
  include MobileBehavior

  @@range = 15
  @@run_delay = 500
  
  def onSpawn(mob)
  end

  def onDoubleClick(me, player)
    return false
  end
  
  def onSpeech(mob, player, line)
  end

  def onHello(me, player)
  end
  
  def onEnterArea(mob, player)
  end
  
  def onAttacked(mob, attacker)
  end
  
  def onDeath(mob, corpse)
  end

  def setStats(mob, stats)
    $api.setAttribute(mob, Attribute::STRENGTH, stats[:str])
    $api.setAttribute(mob, Attribute::FATIGUE, stats[:fatigue])
    $api.setAttribute(mob, Attribute::INTELLIGENCE, stats[:int])
    $api.refreshStats(mob)
  end

  def beAggressiveToThemOnly(me, victim)
    # attack if not already fighting someone else
    return if me.getOpponent() != nil and me.getOpponent() != victim
    attackAndChase(me, victim)
  end

  def beAggressiveToThemAndAll(me, victim)
    # attack if not already fighting someone else
    return if me.getOpponent() != nil and me.getOpponent() != victim
    if not attackAndChase(me, victim)
      # No Success: Search others
      me.setOpponent(nil)
      searchVictims(me)
    end
  end
  
  def attackAndChase(me, victim)
    # Make sure we don't walk too fast
    no_action_before = $api.getObjectProperty(me, "noActionBefore") || 0
    return false if $api.getTimerTicks() < no_action_before
    
    $api.attack(me, victim)
    distance = $api.getDistance(me, victim)
    if(distance > @@range or !victim.isVisible() || !$api.canSee(me, victim))
      return false
    elsif distance > 1
      # victim not arrived yet, run after him
      return false if not $api.runToward(me, victim)
    end

    # Check again (we might arrive the victim or it ran from us, so need another action)
    $api.setObjectProperty(me, "noActionBefore", $api.getTimerTicks() + @@run_delay)
    $api.addTimer(@@run_delay) do 
      attackAndChase(me, victim) if me.getOpponent() == victim
    end
    return true
  end
  
  def searchVictims(me)
    # Check if there is a player to chase
    for player in $api.getNearbyPlayers(me)
      attackAndChase(me, player)
      return
    end
  end

end
