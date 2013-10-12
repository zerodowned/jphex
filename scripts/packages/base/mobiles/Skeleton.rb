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
class Skeleton
  include MobileBehavior
  
  def onSpawn(me)
    $api.setName(me, "a skeleton")
    $api.setGraphic(me, 0x2A)

    $api.setAttribute(me, Attribute::STRENGTH, 25)
    $api.setAttribute(me, Attribute::FATIGUE, 10)
    $api.setAttribute(me, Attribute::INTELLIGENCE, 25)

    $api.setAttribute(me, Attribute::MELEE, 500)
    $api.setAttribute(me, Attribute::BATTLE_DEFENSE, 350)

    $api.refreshStats(me)
  end
  
  def onEnterArea(me, player)
    # Focus on this player - first come, first served :)
    return if $api.getObjectProperty(me, "ignoreIncoming")
    $api.setObjectProperty(me, "ignoreIncoming", true)
    $api.attack(me, player)
    chase(me, player)
  end

  def onDoubleClick(me, player)
    return false
  end

  def onSpeech(mob, player, line)
  end

  def onHello(me, player)
  end

  def chasePlayers(me)
    # Check if there is a player to chase
    for player in $api.getNearbyPlayers(me)
      onEnterArea(me, player)
      return
    end
    # No more players found, wait for incoming players again
    $api.setObjectProperty(me, "ignoreIncoming", false)
  end

  def chase(me, player)
    distance = $api.getDistance(me, player)
    if(distance > 15 or !player.isVisible())
      # out of range, search new victims
      chasePlayers(me)
      return
    elsif distance > 1
      # player not arrived yet, run after him
      $api.runToward(me, player)
    end

    # Check again (we might arrive the player or it ran from us, so chase again)
    $api.addTimer(500) do 
      chase(me, player)
    end
  end
end
