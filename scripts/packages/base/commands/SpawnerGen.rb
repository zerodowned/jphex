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
class SpawnerGen < TextCommand
  @@spawners = [
      # Blacksmith
      {:x => 393, :y => 532, :z => 0, :type => 0x044B, :count => 1, :duration => 10, :range => 0},
      # Provisioner
      {:x => 478, :y => 654, :z => 0, :type => 0x0455, :count => 1, :duration => 10, :range => 0}
    ]
  
  def invoke(player, line)
    for entry in @@spawners
      next if exists?(entry)
      x, y, z, = entry[:x], entry[:y], entry[:z]
      puts "Creating spawner at #{x} #{y} #{z}"
      spawner = $api.createItemAtLocation(x, y, z, entry[:type])
      $api.setObjectProperty(spawner, "count", entry[:count])
      $api.setObjectProperty(spawner, "duration", entry[:duration])
      $api.setObjectProperty(spawner, "range", entry[:range])
      spawner.setBehavior("spawner")
    end
  end
  
  def exists?(entry)
    for item in $api.getItemsAtLocation(entry[:x], entry[:y], entry[:z])
      if item.getGraphic() == entry[:type]
        return true
      end
    end
    return false
  end
end
