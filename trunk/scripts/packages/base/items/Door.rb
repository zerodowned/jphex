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
class Door
  include ItemBehavior
  
  AUTO_CLOSE_TIME = 4000
  
  def onCreate(door)
    # Default state: Closed
    $api.setGraphic(door, 0x3D3)
    $api.setObjectProperty(door, "isOpen", false)
  end

  def onBehaviorChange(door)
    # Reset to default state
    $api.setObjectProperty(door, "isOpen", false)
  end

  def onLoad(door)
    # Close doors on server load
    close(door)
  end
  
  def onUse(player, door)
    if $api.getObjectProperty(door, "isOpen")
      close(door)
    else
      open(door)
    end
  end
  
  def open(door)
    return if $api.getObjectProperty(door, "isOpen")
    toggleGraphic(door)
    playOpenSound(door)
    $api.setObjectProperty(door, "isOpen",  true)
    # closeAt is used when people close and open the door while the timer is active
    $api.setObjectProperty(door, "closeAt", $api.getTimerTicks() + AUTO_CLOSE_TIME)
    $api.addTimer(AUTO_CLOSE_TIME) do
      close(door) if $api.getObjectProperty(door, "closeAt") <= $api.getTimerTicks()
    end
  end
  
  def close(door)
    return if !$api.getObjectProperty(door, "isOpen")
    toggleGraphic(door)
    playCloseSound(door)
    $api.setObjectProperty(door, "isOpen",  false)
  end
  
  def toggleGraphic(door)
    newGraphic = case door.getGraphic()
      when 0x3D3 then 0x3D7
      when 0x3D4 then 0x3D8
      when 0x3D7 then 0x3D3
      when 0x3D8 then 0x3D4
      else door.getGraphic()
    end
    $api.setGraphic(door, newGraphic)
  end
  
  def playOpenSound(door)
    sound = case door.getGraphic()
      when 0x3D3 then 0x42
      when 0x3D4 then 0x42
      when 0x3D7 then 0x42
      when 0x3D8 then 0x42
      else 0x42
    end
    $api.playSoundNearObj(door, sound)
  end

  def playCloseSound(door)
    sound = case door.getGraphic()
      when 0x3D3 then 0x48
      when 0x3D4 then 0x48
      when 0x3D7 then 0x48
      when 0x3D8 then 0x48
      else 0x48
    end
    $api.playSoundNearObj(door, sound)
  end
end
