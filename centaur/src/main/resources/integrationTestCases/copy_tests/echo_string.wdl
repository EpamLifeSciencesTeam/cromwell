workflow echo_string {
    String input_str = "Hello there!"

    # Merge per-interval GVCFs
    call echo {
            input: input_str = input_str
     }

    # Outputs that will be retained when execution is complete
    output {
            File output_file = echo.output_file
    }
}

task echo {
    String input_str
    String output_file_name = "output.txt"

    Int machine_mem_gb = 2
    Int command_mem_gb = machine_mem_gb - 1

    command {
      echo ${input_str} > ${output_file_name}
    }

    runtime {
        docker: "ubuntu"
        memory: "${machine_mem_gb}G"
        cpu: 1
    }

    output {
            File output_file = "${output_file_name}"
    }
}
