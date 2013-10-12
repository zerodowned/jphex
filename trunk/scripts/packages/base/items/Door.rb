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

  def onInit(item)
    $api.setObjectProperty(item, "isOpen", false)
  end
  
  def onUse(player, item)
    if $api.getObjectProperty(item, "isOpen")
      close(item)
    else
      open(item)
    end
  end
  
  def open(item)
    return if $api.getObjectProperty(item, "isOpen")
    playOpenSound(item)
    toggleGraphic(item)
    $api.setObjectProperty(item, "isOpen",  true)
    $api.addTimer(AUTO_CLOSE_TIME) do
        close(item)
    end
  end
  
  def close(item)
    return if !$api.getObjectProperty(item, "isOpen")
    playCloseSound(item)
    toggleGraphic(item)
    $api.setObjectProperty(item, "isOpen",  false)
  end
  
  def toggleGraphic(item)
    newGraphic = case item.getGraphic()
      when 0x3D3 then 0x3D7
      when 0x3D4 then 0x3D8
      when 0x3D7 then 0x3D3
      when 0x3D8 then 0x3D4
      else graphic
    end
    $api.setGraphic(item, newGraphic)
  end
  
  def playOpenSound(item)
    sound = case item.getGraphic()
      when 0x3D3 then 0x42
      when 0x3D4 then 0x42
      when 0x3D7 then 0x42
      when 0x3D8 then 0x42
      else 0x42
    end
    $api.playSoundNearObj(item, sound)
  end

  def playCloseSound(item)
    sound = case item.getGraphic()
      when 0x3D3 then 0x48
      when 0x3D4 then 0x48
      when 0x3D7 then 0x48
      when 0x3D8 then 0x48
      else 0x48
    end
    $api.playSoundNearObj(item, sound)
  end
end
