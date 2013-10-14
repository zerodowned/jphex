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

# General food class: Set this behavior on items to make them edible
class Food
  include ItemBehavior

  def onCreate(food)
  end

  def onBehaviorChange(food)
  end

  def onLoad(food)
  end
    
  def onUse(player, food)
    if(player.tryAccess(food))
      food.consume(1)
      $api.sendSysMessage(player, "You feel less hungry")
    end
  end
end
