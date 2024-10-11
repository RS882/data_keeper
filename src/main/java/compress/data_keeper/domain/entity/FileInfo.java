package compress.data_keeper.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "file_info")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo extends EntityInfo {

    @Column(name = "type")
    private String type;

    @Column(name = "size")
    private Long size;

    @Column(name = "hash", length = 512)
    private String hash;

    @Column(name="is_original_file")
    private Boolean isOriginalFile;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "folder_id")
    private Folder folder;
}
