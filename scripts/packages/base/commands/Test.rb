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
class Test < TextCommand
  def invoke(player, line)
    $api.targetObject(player) do |obj|
      $api.setGraphic(obj, 0xa0)
      cycleGraphic(obj)
    end
  end
  
  def cycleGraphic(obj)
    $api.setGraphic(obj, obj.getGraphic() + 1)
    $api.say(obj, "Graphic: #{obj.getGraphic().to_s(16)}")
    $api.addTimer(750) do
      cycleGraphic(obj)
    end
  end
end
