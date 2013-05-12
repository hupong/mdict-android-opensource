/*
 * Copyright (C) 2012. Rayman Zhang <raymanzhang@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cn.mdict;

import android.content.res.AssetFileDescriptor;
import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * User: Rayman
 * Date: 12-4-27
 * Time: 下午2:57
 */
public class SocketAssetFileDescriptor extends AssetFileDescriptor {

    /**
     * Create a new AssetFileDescriptor from the given values.
     *
     * @param fd          The underlying file descriptor.
     * @param startOffset The location within the file that the asset starts.
     * This must be 0 if length is UNKNOWN_LENGTH.
     * @param length      The number of bytes of the asset, or
     * {@link #UNKNOWN_LENGTH} if it extends to the end of the file.
     */
    private long contentLength = UNKNOWN_LENGTH;

    public SocketAssetFileDescriptor(ParcelFileDescriptor fd, long startOffset, long length) {
        super(fd, startOffset, length);
        contentLength = length;
    }

    @Override
    public FileInputStream createInputStream() throws IOException {
        if (contentLength < 0) {
            return new ParcelFileDescriptor.AutoCloseInputStream(getParcelFileDescriptor());
        }
        return new AutoCloseInputStream(this);
    }
}
