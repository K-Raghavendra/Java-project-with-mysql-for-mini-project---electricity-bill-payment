/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 2.0, as published by the
 * Free Software Foundation.
 *
 * This program is also distributed with certain software (including but not
 * limited to OpenSSL) that is licensed under separate terms, as designated in a
 * particular file or component or in included license documentation. The
 * authors of MySQL hereby grant you an additional permission to link the
 * program and your derivative works with the separately licensed software that
 * they have included with MySQL.
 *
 * Without limiting anything contained in the foregoing, this file, which is
 * part of MySQL Connector/J, is also subject to the Universal FOSS Exception,
 * version 1.0, a copy of which can be found at
 * http://oss.oracle.com/licenses/universal-foss-exception.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License, version 2.0,
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301  USA
 */

package com.mysql.cj.protocol.x;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.mysql.cj.exceptions.WrongArgumentException;

/**
 * Handle state necessary to accumulate noticed and build a {@link StatementExecuteOk} response.
 */
public class StatementExecuteOkBuilder {
    private long rowsAffected = 0;
    private Long lastInsertId = null;
    private List<String> generatedIds = Collections.emptyList();
    private List<com.mysql.cj.protocol.Warning> warnings = new ArrayList<>();

    public void addNotice(Notice notice) {
        if (notice.getType() == Notice.XProtocolNoticeFrameType_WARNING) {
            this.warnings.add(notice);
        } else if (notice.getType() == Notice.XProtocolNoticeFrameType_SESS_STATE_CHANGED) {
            switch (notice.getParamType()) {
                case Notice.SessionStateChanged_GENERATED_INSERT_ID:
                    // TODO: handle > 2^63-1?
                    this.lastInsertId = notice.getValue().getVUnsignedInt();
                    break;
                case Notice.SessionStateChanged_ROWS_AFFECTED:
                    // TODO: handle > 2^63-1?
                    this.rowsAffected = notice.getValue().getVUnsignedInt();
                    break;
                case Notice.SessionStateChanged_PRODUCED_MESSAGE:
                    // TODO do something with notices. expose them to client
                    //System.err.println("Ignoring NOTICE message: " + msg.getValue().getVString().getValue().toStringUtf8());
                    break;
                case Notice.SessionStateChanged_GENERATED_DOCUMENT_IDS:
                    this.generatedIds = notice.getValueList().stream().map(v -> v.getVOctets().getValue().toStringUtf8()).collect(Collectors.toList());
                    break;
                case Notice.SessionStateChanged_CURRENT_SCHEMA:
                case Notice.SessionStateChanged_ACCOUNT_EXPIRED:
                case Notice.SessionStateChanged_ROWS_FOUND:
                case Notice.SessionStateChanged_ROWS_MATCHED:
                case Notice.SessionStateChanged_TRX_COMMITTED:
                case Notice.SessionStateChanged_TRX_ROLLEDBACK:
                    // TODO: propagate state
                default:
                    // TODO: log warning normally instead of sysout
                    new WrongArgumentException("unhandled SessionStateChanged notice! " + notice).printStackTrace();
            }
        } else {
            // TODO log error normally instead of sysout
            new WrongArgumentException("Got an unknown notice: " + notice).printStackTrace();
        }
    }

    public StatementExecuteOk build() {
        return new StatementExecuteOk(this.rowsAffected, this.lastInsertId, this.generatedIds, this.warnings);
    }
}
