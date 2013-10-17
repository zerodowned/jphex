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
require './scripts/packages/base/mobiles/BaseMobile'
class Guard < BaseMobile
  include MobileBehavior
  
  def onSpawn(mob)
    $api.setName(mob, "a guard")
    $api.setGraphic(mob, 0x2E)

    setStats(mob, :str => 100, :fatigue => 100, :int => 25)

    $api.setAttribute(mob, Attribute::MELEE, 800)
    $api.setAttribute(mob, Attribute::BATTLE_DEFENSE, 1000)
    $api.setAttribute(mob, Attribute::MAGIC_DEFENSE, 500)
  end

  def onEnterArea(me, player)
    $api.say(me, "Greetings, citizen #{player.getName()}!")
  end

  def onAttacked(me, attacker)
    beAggressiveToThemOnly(me, attacker)
  end
end
