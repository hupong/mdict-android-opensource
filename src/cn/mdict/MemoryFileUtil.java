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
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MemoryFileUtil {
    private static final Method sMethodGetParcelFileDescriptor;
    private static final Method sMethodGetFileDescriptor;
    private static final Method sMethodFromMemoryFile;
    private static final Method sMethodFromData;
    private static final Method sMethodDeactivate;
    static {
        sMethodGetParcelFileDescriptor = get(MemoryFile.class, "getParcelFileDescriptor");
        sMethodGetFileDescriptor = get(MemoryFile.class, "getFileDescriptor");
        sMethodFromMemoryFile = get(AssetFileDescriptor.class, "fromMemoryFile", new Class[] {MemoryFile.class});
        sMethodFromData = get(ParcelFileDescriptor.class, "fromData", new Class[] {byte[].class, String.class});
        sMethodDeactivate = get(MemoryFile.class, "deactivate");
    }

    public static ParcelFileDescriptor getParcelFileDescriptor(MemoryFile file) {
        try {
            return (ParcelFileDescriptor) sMethodGetParcelFileDescriptor.invoke(file);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static FileDescriptor getFileDescriptor(MemoryFile file) {
        try {
            return (FileDescriptor) sMethodGetFileDescriptor.invoke(file);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static AssetFileDescriptor fromMemoryFile(MemoryFile file){
        try {
            return (AssetFileDescriptor) sMethodFromMemoryFile.invoke(AssetFileDescriptor.class, file);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deactivate(MemoryFile file){
        try {
            if ( sMethodDeactivate!=null )
                sMethodDeactivate.invoke(file);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static ParcelFileDescriptor fromData(byte[] data, String name)  throws IOException {
        try {
            return (ParcelFileDescriptor) sMethodFromData.invoke(ParcelFileDescriptor.class, data, name);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Method get(Class classObj, String name, Class<?>... parameterTypes) {
        try {
            return classObj.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}