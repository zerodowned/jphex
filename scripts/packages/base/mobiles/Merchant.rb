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
class Merchant
  include MobileBehavior
  
  def onSpawn(mob)
    if rand() < 0.5
      $api.setGraphic(mob, 0x00)
    else
      $api.setGraphic(mob, 0x01)
    end

    $api.assignRandomName(mob, "")
    $api.createClothes(mob)

    $api.setAttribute(mob, Attribute::STRENGTH, 25)
    $api.setAttribute(mob, Attribute::FATIGUE, 10)
    $api.setAttribute(mob, Attribute::INTELLIGENCE, 25)
    $api.refreshStats(mob)
    
    # Fill inventory
    $api.createItemInBackpack(mob, 0x0011) # apple
    $api.createItemInBackpack(mob, 0x0039) # peach
    $api.createItemInBackpack(mob, 0x00B3) # candle stick
    $api.createItemInBackpack(mob, 0x028E) # quarrel
  end

  def onEnterArea(mob, player)
    $api.say(mob, "Hello, " + player.getName())
  end
  
  def onSpeech(mob, player, line)
    if line == mob.getName().downcase + " buy"
      if player.distanceTo(mob) < 4
        $api.say(mob, "Have a look at my wares")
        $api.offerShop(mob, player)
      else
        $api.say(mob, player.getName() + ", please come a littler closer")
      end
    end
  end
end
