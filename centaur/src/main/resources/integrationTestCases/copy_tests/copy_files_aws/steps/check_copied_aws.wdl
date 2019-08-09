import "../../echo_string.wdl" as sub

workflow check_copied_aws {

   call sub.echo_string

   output {
      File output_file = echo_string.output_file
   }

    String filename = "s3://cromwell-bucket-ssvitkov/wf_result/output.txt"
    String filename1 = "s3://cromwell-bucket-ssvitkov/wf_logs/MergeGVCFs-stderr.log"
    String filename2 = "s3://cromwell-bucket-ssvitkov/wf_logs/MergeGVCFs-stdout.log"

    String directory_name = "s3://my-bucket-name-34/wf_logs"

    call CheckFiles {
        input: filename = filename,
            filename1 = filename1,
            filename2 = filename2,
            directory_name = directory_name
    }
}

task CheckFiles {

    String filename
    String filename1
    String filename2
    String directory_name

    Int machine_mem_gb = 2
    Int command_mem_gb = machine_mem_gb - 1

    command {
        //does output.txt exist
        ( if [ -f ${filename} ]; then echo "$filename exists"; fi)

        //does workflow logs exist
        cd ~/${directory_name}
        ( if [ -z "$(ls)" ]; then echo "workflow logs are existing \n ls output: $(ls)"; else echo "something wrong"; fi; )

        //does MergeGVCFs-stderr.log and MergeGVCFs-stdout.log exist
        ( if [ -f ${filename1} ]; then echo "$filename1 exists"; else echo "something wrong"; fi; )
        ( if [ -f ${filename2} ]; then echo "$filename2 exists"; else echo "something wrong"; fi; )
    }


    runtime {
        docker: "ubuntu"
        memory: "${machine_mem_gb}G"
        cpu: 1
    }
}
