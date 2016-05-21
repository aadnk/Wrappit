/**
 * Copyright (C) dmulloy2 <http://dmulloy2.net>
 * Copyright (C) Kristian S. Strangeland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.comphenix.wrappit.minecraft;

import java.lang.reflect.Field;
import java.util.List;

import com.comphenix.protocol.PacketType;

public class CodePacketInfo {
	private final List<Field> memoryOrder;
	private final List<Field> networkOrder;
	private final PacketType type;
	
	public CodePacketInfo(List<Field> memoryOrder, List<Field> networkOrder, PacketType type) {
		this.memoryOrder = memoryOrder;
		this.networkOrder = networkOrder;
		this.type = type;
	}

	/**
	 * Determine if the memory and network contain the same number of fields.
	 * <p>
	 * If not, we may have to do some manual work.
	 * @return TRUE if the field count is the same, FALSE otherwise.
	 */
	public boolean isBalanced() {
		return memoryOrder.size() == networkOrder.size();
	}
	
	public List<Field> getMemoryOrder() {
		return memoryOrder;
	}

	public List<Field> getNetworkOrder() {
		return networkOrder;
	}

	public PacketType getType() {
		return type;
	}
}
