/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package mobisocial.bento.anyshare.io;

import mobisocial.socialkit.musubi.DbObj;

public class ItemObject {
    public static final String TABLE = "items";
    public static final String _ID = "_id";
    public static final String FEEDNAME = "feedname";
    public static final String TITLE = "title";
    public static final String DESC = "desc";
    public static final String TIMESTAMP = DbObj.COL_TIMESTAMP;
	public static final String RAW = DbObj.COL_RAW;
	public static final String PARENT_ID = "parent_id";
    public static final String OBJHASH = "hash";

    public long _id;
    public String feedname;
    public String title;
    public String desc;
    public long hash;
    public Integer timestamp;
    public byte[] mRaw;
    public long parent_id;

}