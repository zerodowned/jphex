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
require './scripts/magery/BaseSpellHandler'
class Fireball < BaseSpellHandler

  @@delay = 2000
  
  def castOn(player, scroll, target)
    if player.distanceTo(target) > 10
      $api.sendSysMessage(player, "That is too far away.")
      return
    end
    if !player.canSee(target)
      $api.sendSysMessage(player, "You can't see that.")
      return
    end    

    beginCast(player, Spell::FIREBALL, scroll, 20, @@delay, 100, 300) do
      damage = player.getAttribute(Attribute::INTELLIGENCE) / 5
      sound = 0xA2
      if $api.checkSkill(target, Attribute::MAGIC_DEFENSE, 0, 1100)
        $api.sendSysMessage(target, "You feel yourself resisting magical energy!")
        damage = damage / 3
        sound = 0xA1
      end
      damage = 1 if damage <= 0
      
      $api.throwFireball(player, target)
      $api.playSoundNearObj(player, sound)
      target.dealDamage(damage)      
    end
  end
end
