/*
 This file is part of Libresonic.

 Libresonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libresonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Libresonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.ajax;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import org.libresonic.player.Logger;
import org.libresonic.player.domain.MediaFile;
import org.libresonic.player.service.MediaFileService;
import org.libresonic.player.service.metadata.MetaData;
import org.libresonic.player.service.metadata.MetaDataParser;
import org.libresonic.player.service.metadata.MetaDataParserFactory;

/**
 * Provides AJAX-enabled services for editing tags in music files.
 * This class is used by the DWR framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
public class TagService {

    private static final Logger LOG = Logger.getLogger(TagService.class);

    private MetaDataParserFactory metaDataParserFactory;
    private MediaFileService mediaFileService;

    /**
     * Updated tags for a given music file.
     *
     * @param id     The ID of the music file.
     * @param track  The track number.
     * @param artist The artist name.
     * @param album  The album name.
     * @param title  The song title.
     * @param year   The release year.
     * @param genre  The musical genre.
     * @return "UPDATED" if the new tags were updated, "SKIPPED" if no update was necessary.
     *         Otherwise the error message is returned.
     */
    public String setTags(int id, String track, String artist, String album, String title, String year, String genre) {

        track = StringUtils.trimToNull(track);
        artist = StringUtils.trimToNull(artist);
        album = StringUtils.trimToNull(album);
        title = StringUtils.trimToNull(title);
        year = StringUtils.trimToNull(year);
        genre = StringUtils.trimToNull(genre);

        Integer trackNumber = null;
        if (track != null) {
            try {
                trackNumber = new Integer(track);
            } catch (NumberFormatException x) {
                LOG.warn("Illegal track number: " + track, x);
            }
        }

        Integer yearNumber = null;
        if (year != null) {
            try {
                yearNumber = new Integer(year);
            } catch (NumberFormatException x) {
                LOG.warn("Illegal year: " + year, x);
            }
        }

        try {

            MediaFile file = mediaFileService.getMediaFile(id);
            MetaDataParser parser = metaDataParserFactory.getParser(file.getFile());

            if (!parser.isEditingSupported()) {
                return "Tag editing of " + FilenameUtils.getExtension(file.getPath()) + " files is not supported.";
            }

            if (StringUtils.equals(artist, file.getArtist()) &&
                    StringUtils.equals(album, file.getAlbumName()) &&
                    StringUtils.equals(title, file.getTitle()) &&
                    ObjectUtils.equals(yearNumber, file.getYear()) &&
                    StringUtils.equals(genre, file.getGenre()) &&
                    ObjectUtils.equals(trackNumber, file.getTrackNumber())) {
                return "SKIPPED";
            }

            MetaData newMetaData = parser.getMetaData(file.getFile());

            // Note: album artist is intentionally set, as it is not user-changeable.
            newMetaData.setArtist(artist);
            newMetaData.setAlbumName(album);
            newMetaData.setTitle(title);
            newMetaData.setYear(yearNumber);
            newMetaData.setGenre(genre);
            newMetaData.setTrackNumber(trackNumber);
            parser.setMetaData(file, newMetaData);
            mediaFileService.refreshMediaFile(file);
            mediaFileService.refreshMediaFile(mediaFileService.getParentOf(file));
            return "UPDATED";

        } catch (Exception x) {
            LOG.warn("Failed to update tags for " + id, x);
            return x.getMessage();
        }
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setMetaDataParserFactory(MetaDataParserFactory metaDataParserFactory) {
        this.metaDataParserFactory = metaDataParserFactory;
    }
}
