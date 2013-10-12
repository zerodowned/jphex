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

    $api.setAttribute(me, Attribute::MELEE, 250)
    $api.setAttribute(me, Attribute::BATTLE_DEFENSE, 300)

    $api.refreshStats(me)
  end
  
  def onEnterArea(me, player)
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
  
  def chase(me, player)
    distance = $api.getDistance(me, player)
    return if(distance > 15 or !player.isVisible())

    if distance > 1
      $api.runToward(me, player)
    end
    
    # To check if player ran away
    $api.addTimer(500) do 
      chase(me, player)
    end
  end
end
