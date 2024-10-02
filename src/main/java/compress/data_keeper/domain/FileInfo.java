package compress.data_keeper.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_info")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo extends EntityInfo {

    @Column(name = "type")
    private String type;

    @Column(name = "size")
    private Long size;

    @Column(name = "hash")
    private String hash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "folder_id")
    private Folder folder;
}
