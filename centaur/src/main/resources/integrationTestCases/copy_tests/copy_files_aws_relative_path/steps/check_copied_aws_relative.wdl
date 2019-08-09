workflow check_copied_aws_relative {
    String filename = "wf_results/output.txt"
    String directory_name = "wf_logs"

    call CheckFiles {
        input:
            filename = filename,
            directory_name = directory_name
    }
}

task CheckFiles {
    String filename
    String directory_name

    Int machine_mem_gb = 2
    Int command_mem_gb = machine_mem_gb - 1

    command {
       #does output.txt exist
       ( if [ -f ~/${filename} ]; then echo "$filename exists"; else exit 1; fi)

       #does workflow logs exist
       cd ~/${directory_name}
      ( if [ -z "$(ls)" ]; then echo "workflow logs exists,\n ls output: \n $(ls)"; else exit 1; fi; )

       #does stderr.log and stdout.log exist
       ( if [ -f ~/$(find cl_logs/ -name 'echo-stderr*') ]; then echo "file stderr exists"; else exit 1; fi)
       ( if [ -f ~/$(find cl_logs/ -name 'echo-stdout*') ]; then echo "file stdout exists"; else exit 1; fi)
    }

    runtime {
        docker: "ubuntu"
        memory: "${machine_mem_gb}G"
        cpu: 1
    }
}
