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
class Deer
  include MobileBehavior
  
  def onSpawn(mob)
    $api.setName(mob, "a deer")
    $api.setGraphic(mob, 0x34)

    $api.setAttribute(mob, Attribute::STRENGTH, 25)
    $api.setAttribute(mob, Attribute::FATIGUE, 10)
    $api.setAttribute(mob, Attribute::INTELLIGENCE, 25)

    $api.setAttribute(mob, Attribute::MELEE, 250)
    $api.setAttribute(mob, Attribute::BATTLE_DEFENSE, 300)

    $api.refreshStats(mob)
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
end
