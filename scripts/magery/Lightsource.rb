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
class Lightsource < BaseSpellHandler
  
  @@delay = 2000
  
  def castAt(player, scroll, target)
    beginCast(player, Spell::LIGHTSOURCE, scroll, 20, @@delay, 300, 500) do
      lightsource = $api.createItemAtLocation(target.getX(), target.getY(), target.getZ(), 0x1B3)
      duration = player.getAttribute(Attribute::INTELLIGENCE) * 1000 / 3
      $api.addTimer(duration) do
        lightsource.delete()
      end
    end
  end
end
